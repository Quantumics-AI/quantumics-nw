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

@Component({
  selector: 'app-data-quality-list',
  templateUrl: './data-quality-list.component.html',
  styleUrls: ['./data-quality-list.component.scss']
})
export class DataQualityListComponent implements OnInit {

  private certificate$: Observable<Certificate>;
  private unsubscribe: Subject<void> = new Subject();
  private certificateData: Certificate;
  public dataQualityList: Array<DataQuailtyListResponse> = [];

  public projectId: number;
  userId: number;

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

    const dataQuality1 = {
      id: 1,
      ruleName: 'DQ Name1',
      ruleType: 'Data completeness-Row',
      ruleDescription: 'This is added test rules',
      createdBy: 'Naveen',
      createdDate: new Date(),
      modifiedDate: new Date(),
      status: 'Active'

    } as DataQuailtyListResponse;

    this.dataQualityList.push(dataQuality1);
  }

  public edit(dataQuality: DataQuailtyListResponse): void {
    const modalRef = this.modalService.open(EditRuleComponent, { size: 'lg', windowClass: 'modal-size', scrollable: false });
    modalRef.componentInstance.userId = this.userId;
    modalRef.componentInstance.projectId = this.projectId;
    modalRef.componentInstance.ruleData = dataQuality;


    modalRef.result.then((result) => {
      
    }, (result) => {
      
    });



    // sessionStorage.setItem('editDataQuality', JSON.stringify(dataQuality));
    // this.router.navigate([`projects/${this.projectId}/data-quality/edit`]);
  }
}
