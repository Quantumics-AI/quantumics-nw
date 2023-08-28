import { Component, OnInit } from '@angular/core';
import { DataQuailtyListResponse } from '../../models/data-quality-list-response';
import { ActivatedRoute, Router } from '@angular/router';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { Observable, Subject } from 'rxjs';
import { Certificate } from 'src/app/models/certificate';
import { Quantumfacade } from 'src/app/state/quantum.facade';
import { takeUntil } from 'rxjs/operators';
import { EditRuleComponent } from '../edit-rule/edit-rule.component';
import { SnackbarService } from 'src/app/core/services/snackbar.service';
import { ViewRunningRulesComponent } from '../view-running-rules/view-running-rules.component';

@Component({
  selector: 'app-data-quality-list',
  templateUrl: './data-quality-list.component.html',
  styleUrls: ['./data-quality-list.component.scss']
})
export class DataQualityListComponent implements OnInit {

  private certificate$: Observable<Certificate>;
  private unsubscribe: Subject<void> = new Subject();
  private certificateData: Certificate;
  public dataQualityList: Array<DataQuailtyListResponse> = [
    {
      id: 1,
      ruleName: 'DQ Name1',
      ruleType: 'Data completeness-Row',
      ruleDescription: 'This is added test rules',
      createdBy: 'Naveen',
      createdDate: new Date(),
      modifiedDate: new Date(),
      status: 'Active',
      selected: false
    },
    {
      id: 2,
      ruleName: 'DQ Name2',
      ruleType: 'Data completeness-Row',
      ruleDescription: 'This is added test rules',
      createdBy: 'Naveen',
      createdDate: new Date(),
      modifiedDate: new Date(),
      status: 'Active',
      selected: false
    },
    {
      id: 3,
      ruleName: 'DQ Name3',
      ruleType: 'Data completeness-Row',
      ruleDescription: 'This is added test rules',
      createdBy: 'Naveen',
      createdDate: new Date(),
      modifiedDate: new Date(),
      status: 'Active',
      selected: false
    },
    {
      id: 4,
      ruleName: 'DQ Name4',
      ruleType: 'Data completeness-Row',
      ruleDescription: 'This is added test rules',
      createdBy: 'Naveen',
      createdDate: new Date(),
      modifiedDate: new Date(),
      status: 'Active',
      selected: false
    },
    {
      id: 5,
      ruleName: 'DQ Name5',
      ruleType: 'Data completeness-Row',
      ruleDescription: 'This is added test rules',
      createdBy: 'Naveen',
      createdDate: new Date(),
      modifiedDate: new Date(),
      status: 'Active',
      selected: false
    }
  ];

  public projectId: number;
  userId: number;
  selectAllChecked: boolean = false;
  anyCheckboxSelected: boolean = false;

  // pagination
  public startIndex: number = 0;
  public pageSize: number = 10;
  public endIndex: number = this.pageSize;
  public page = 1;

  constructor(
    private readonly router: Router,
    private readonly activatedRoute: ActivatedRoute,
    private modalService: NgbModal,
    private quantumFacade: Quantumfacade,
    private snakbar: SnackbarService) {
    this.projectId = +this.activatedRoute.snapshot.paramMap.get('projectId');
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

    // const dataQuality1 = {
    //   id: 1,
    //   ruleName: 'DQ Name1',
    //   ruleType: 'Data completeness-Row',
    //   ruleDescription: 'This is added test rules',
    //   createdBy: 'Naveen',
    //   createdDate: new Date(),
    //   modifiedDate: new Date(),
    //   status: 'Active'

    // } as DataQuailtyListResponse;

    // this.dataQualityList.push(dataQuality1);
  }

  public edit(dataQuality: DataQuailtyListResponse): void {

    this.router.navigate([`projects/${this.projectId}/data-quality/create`]);
    // const modalRef = this.modalService.open(EditRuleComponent, { size: 'lg', windowClass: 'modal-size', scrollable: false });
    // modalRef.componentInstance.userId = this.userId;
    // modalRef.componentInstance.projectId = this.projectId;
    // modalRef.componentInstance.ruleData = dataQuality;


    // modalRef.result.then((result) => {
      
    // }, (result) => {
      
    // });



    // sessionStorage.setItem('editDataQuality', JSON.stringify(dataQuality));
    // this.router.navigate([`projects/${this.projectId}/data-quality/edit`]);
  }

  public viewRunning(): void {
    this.router.navigate([`projects/${this.projectId}/data-quality/view-rules`]);
    // const modalRef = this.modalService.open(ViewRunningRulesComponent, { size: 'lg', windowClass: 'modal-size', scrollable: false });
    // modalRef.componentInstance.userId = this.userId;
    // modalRef.componentInstance.projectId = this.projectId;


    // modalRef.result.then((result) => {
      
    // }, (result) => {
      
    // });
  }

  selectRuleAll(event: Event): void {
    const isChecked = (event.target as HTMLInputElement).checked;
    this.selectAllChecked = isChecked;
    this.anyCheckboxSelected = isChecked;
    // Loop through your dataQualityList and update the selected state of each item
    this.dataQualityList.map(dataQuality => {
      dataQuality.selected = isChecked;
    });
  }
  

  selectRule(evt: Event, dataQuality: DataQuailtyListResponse): void {
    dataQuality.selected = (evt.target as HTMLInputElement).checked;
  // Check if any checkbox is selected
  this.anyCheckboxSelected = this.dataQualityList.some(item => item.selected);
    // Check if all checkboxes are selected and update the "Select All" checkbox accordingly
    this.selectAllChecked = this.dataQualityList.every(item => item.selected);
  }

  public runRule(): void {

  }
  

  // pagination 
  public onPageChange(currentPage: number): void {
    this.startIndex = (currentPage - 1) * this.pageSize;
    this.endIndex = this.startIndex + this.pageSize;
  }
}
