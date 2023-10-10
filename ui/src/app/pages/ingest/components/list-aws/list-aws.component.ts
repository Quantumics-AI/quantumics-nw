import { Component, OnInit, EventEmitter, Output, Input } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { Observable, Subject } from 'rxjs';
import { Certificate } from 'src/app/models/certificate';
import { Quantumfacade } from 'src/app/state/quantum.facade';
import { takeUntil } from 'rxjs/operators';
import { SourceDataService } from '../../services/source-data.service';
import { SnackbarService } from 'src/app/core/services/snackbar.service';
import { AwsConfirmationComponent } from '../aws-confirmation/aws-confirmation.component';
import { EditDataSourceComponent } from '../edit-data-source/edit-data-source.component';

@Component({
  selector: 'app-list-aws',
  templateUrl: './list-aws.component.html',
  styleUrls: ['./list-aws.component.scss']
})
export class ListAwsComponent {

  private certificate$: Observable<Certificate>;
  private unsubscribe: Subject<void> = new Subject();
  private certificateData: Certificate;

  projectId: string;
  userId: number;
  loading: boolean;

  searchTerm: any = { connectionName: '' };
  public searchDiv: boolean = false;
  public searchString: string;
  public startIndex: number = 0;
  public pageSize: number = 10;
  public endIndex: number = this.pageSize;
  public page = 1;
  // public awsListData = [
  //   {id:1, name : "AWS1", access_type: "IAM", role: "user", iam_role: "Naveen", createdDate: new Date(), modifiedDate: new Date() },
  //   {id:2, name : "AWS2", access_type: "IAM", role: "user", iam_role: "Naveen", createdDate: new Date(), modifiedDate: new Date() },
  //   {id:3, name : "AWS3", access_type: "IAM", role: "user", iam_role: "Naveen", createdDate: new Date(), modifiedDate: new Date() },
  //   {id:4, name : "AWS4", access_type: "IAM", role: "user", iam_role: "Naveen", createdDate: new Date(), modifiedDate: new Date() },
  //   {id:5, name : "AWS5", access_type: "IAM", role: "user", iam_role: "Naveen", createdDate: new Date(), modifiedDate: new Date() }
  // ];

  public sourceListData: any;
  public isDescending: boolean;

  constructor(
    private router: Router,
    private activatedRoute: ActivatedRoute,
    private modalService: NgbModal,
    private quantumFacade: Quantumfacade,
    private sourceDataService: SourceDataService,
    private snakbar: SnackbarService
  ){
    this.certificate$ = this.quantumFacade.certificate$;
    this.certificate$
      .pipe(takeUntil(this.unsubscribe))
      .subscribe(certificate => {
        if (certificate) {
          this.certificateData = certificate;
          this.userId = +this.certificateData.user_id;
        }
      });
  }

  ngOnInit(): void {
    this.projectId = localStorage.getItem('project_id');
    this.getAwsList();
  }

  public getAwsList(): void {
    this.loading = true;
    this.sourceDataService.getSourceData(+this.projectId, this.userId).subscribe((response) => {
      this.loading = false;
      console.log("getAws", response);
      this.sourceListData = response;
      if (this.sourceListData.length > 1) {
        this.sourceListData.sort((val1, val2) => {
          return (
            (new Date(val2.createdDate) as any) -
            (new Date(val1.createdDate) as any)
          );
        });
      }
    }, (error) => {
      this.loading = false;
    })
  }

  parseConnectionData(connectionData: string): { iam: string } | null {
    try {
        const parsedData = JSON.parse(connectionData);
        return parsedData;
    } catch (error) {
        return null; // Handle parsing errors if needed
    }
  }

  // Sort data 
  public sortSorceData(): void {
    this.isDescending = !this.isDescending;

    if (this.isDescending) {
      this.sourceListData = this.sourceListData.sort((a, b) => {
        var source_name_order = a.connectionName.localeCompare(b.connectionName);
        return source_name_order;
      });
    } else {
      this.sourceListData = this.sourceListData.sort((a, b) => {
        var source_name_order = b.connectionName.localeCompare(a.connectionName);
        return source_name_order;
      });
    }

  }

  public sortAccessType(): void {
    this.isDescending = !this.isDescending;

    if (this.isDescending) {
      this.sourceListData = this.sourceListData.sort((a, b) => {
        var access = a.accessType.localeCompare(b.accessType);
        return access;
      });
    } else {
      this.sourceListData = this.sourceListData.sort((a, b) => {
        var access = b.accessType.localeCompare(a.accessType);
        return access;
      });
    }
  }

  sortDataByCreatedDate() {
    this.isDescending = !this.isDescending;
    if (this.isDescending) {
      this.sourceListData.sort((a, b) => a.createdDate - b.createdDate);
    } else {
      this.sourceListData.sort((a, b) => b.createdDate - a.createdDate);
    }
    
  }

  sortDataByModifiedDate(){
    this.isDescending = !this.isDescending;
    if (this.isDescending) {
      this.sourceListData.sort((a, b) => a.modifiedDate - b.modifiedDate);
    } else {
      this.sourceListData.sort((a, b) => b.modifiedDate - a.modifiedDate);
    }
  }

  updatedSourceData(data: any): void {
    const index = this.sourceListData.findIndex(x => x.id === data?.id);
    this.sourceListData[index] = data;
  }

  public edit(d: any): void {
    // console.log(d);
    // const connectionData = JSON.parse(d.connectionData);
    const modalRef = this.modalService.open(EditDataSourceComponent, { size: 'md', scrollable: false });
    modalRef.componentInstance.userId = this.userId;
    modalRef.componentInstance.projectId = this.projectId;
    modalRef.componentInstance.awsId = d.id;
    modalRef.componentInstance.sourceData = d;
    // modalRef.componentInstance.rolename = connectionData.iam;

    modalRef.result.then((result) => {
      this.updatedSourceData(result);
      this.getAwsList();
    }, (result) => {
      
     });
  }

  public delete(id:number): void {
    const modalRef = this.modalService.open(AwsConfirmationComponent, { size: 'md modal-dialog-centered', scrollable: false });
    modalRef.componentInstance.userId = this.userId;
    modalRef.componentInstance.projectId = this.projectId;
    modalRef.componentInstance.awsId = id;

    modalRef.result.then((result) => {
      this.loading = true;
      this.sourceDataService.deleteSourceData(+this.projectId, this.userId, id).subscribe((res:any) => {
        this.loading = false;
        this.snakbar.open("Data source deleted successfully!");
        const idx = this.sourceListData.findIndex(x => x.id === id);
        this.sourceListData.splice(idx, 1);
        this.getAwsList();
      }, (error) => {
        this.loading = false;
        this.snakbar.open(error);
      });
      
    }, (result) => {
      
     });
  }

  searchInput(str) {
    this.searchString = str;
    if (str.length == 0) {
      this.searchDiv = false;
    } else {
      this.searchDiv = true;
    }
  }

  clearSearhInput() {
    this.searchTerm = { connectionName: '' };
    this.searchDiv = false;
  }

  public onPageChange(currentPage: number): void {
    this.startIndex = (currentPage - 1) * this.pageSize;
    this.endIndex = this.startIndex + this.pageSize;
  }

}
